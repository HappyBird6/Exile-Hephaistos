import fixture from '../../../../../contracts/support-goal-filter-v1/fixtures.json'
import { groupTypes, weighted } from './types'
import type { Catalog, GoalFilterAdapter, Issue } from './types'
export const fixtureContext = fixture.apiExamples.validateRequest.context
export const mockGoalFilterAdapter: GoalFilterAdapter = {
  id: 'synthetic-contract-v1',
  mock: true,
  async catalog(context, signal) {
    signal.throwIfAborted()
    const supported =
      context.baseItemId === fixtureContext.baseItemId &&
      context.snapshotId === fixtureContext.snapshotId
    return {
      version: 1,
      catalogVersion: fixture.catalogVersion,
      context,
      groupTypes: groupTypes.map((type) => ({
        type,
        evaluation: 'SUPPORTED',
        probability: 'UNSUPPORTED',
        reasonCode: 'NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED',
      })),
      stats: supported
        ? fixture.stats.map((stat) => ({
            ...stat,
            kind: stat.kind as Catalog['stats'][number]['kind'],
            label: stat.statId.split('.').at(-1)!.replaceAll('_', ' '),
            support: {
              evaluation: 'SUPPORTED',
              probability: 'UNSUPPORTED',
              reasonCode: 'NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED',
            },
            eligible: true,
            eligibilityReason: null,
            sourceStatIds: stat.contributions.map((source) => source.statId),
            sourceUrls: [],
          }))
        : [],
      issues: supported
        ? []
        : [
            {
              code: 'UNSUPPORTED_BASE',
              path: '/context/baseItemId',
              message: 'Synthetic mock supports only fixture-base.',
              severity: 'WARNING',
            },
          ],
    }
  },
  async validate(context, goal, signal) {
    const catalog = await mockGoalFilterAdapter.catalog(context, signal)
    const issues: Issue[] = []
    const issue = (code: string, path: string) =>
      issues.push({ code, path, message: code, severity: 'ERROR' })
    if (goal.version !== 1) issue('UNSUPPORTED_VERSION', '/version')
    if (goal.catalogVersion !== catalog.catalogVersion)
      issue('CATALOG_VERSION_MISMATCH', '/catalogVersion')
    if (goal.general.baseItemId !== context.baseItemId)
      issue('BASE_MISMATCH', '/general/baseItemId')
    const range = (
      value: { min: number | null; max: number | null },
      path: string,
    ) => {
      if (
        [value.min, value.max].some((n) => n !== null && !Number.isFinite(n)) ||
        (value.min !== null && value.max !== null && value.min > value.max)
      )
        issue('INVALID_RANGE', path)
    }
    range(goal.general.itemLevel, '/general/itemLevel')
    if (!goal.groups.some((g) => !g.disabled)) issue('EMPTY_GOAL', '/groups')
    const ids = new Set<string>()
    goal.groups.forEach((g, i) => {
      const path = `/groups/${i}`
      if (ids.has(g.id)) issue('DUPLICATE_ID', path)
      ids.add(g.id)
      const active = g.entries.filter((e) => !e.disabled)
      if (!g.disabled && !active.length) issue('EMPTY_GROUP', path)
      if ((weighted(g.type) || g.type === 'COUNT') !== (g.range !== null))
        issue('INVALID_GROUP_RANGE', `${path}/range`)
      if (g.range) range(g.range, `${path}/range`)
      if (
        g.type === 'COUNT' &&
        g.range &&
        [g.range.min, g.range.max].some(
          (n) => n !== null && (!Number.isInteger(n) || n < 0),
        )
      )
        issue('INVALID_COUNT_RANGE', `${path}/range`)
      if (
        g.type === 'COUNT' &&
        g.range?.max !== null &&
        g.range?.max !== undefined &&
        g.range.max > active.length
      )
        issue('INVALID_COUNT_RANGE', `${path}/range`)
      const stats = new Set<string>()
      g.entries.forEach((e, j) => {
        const ep = `${path}/entries/${j}`
        if (ids.has(e.id)) issue('DUPLICATE_ID', ep)
        ids.add(e.id)
        if (stats.has(e.statId)) issue('DUPLICATE_STAT', ep)
        stats.add(e.statId)
        range(e.range, `${ep}/range`)
        const stat = catalog.stats.find((s) => s.statId === e.statId)
        if (!stat)
          issues.push({
            code: 'UNKNOWN_STAT',
            path: ep,
            message: 'Stat is not available in this catalog.',
            severity: g.disabled || e.disabled ? 'WARNING' : 'ERROR',
          })
        else if (e.unit !== stat.unit) issue('UNIT_MISMATCH', ep)
        if (
          g.type.startsWith('WEIGHTED') &&
          (e.weight === null || !Number.isFinite(e.weight))
        )
          issue('INVALID_WEIGHT', ep)
        if (!weighted(g.type) && e.weight !== null) issue('INVALID_WEIGHT', ep)
      })
    })
    return {
      version: 1,
      valid: !issues.some((i) => i.severity === 'ERROR'),
      issues,
      capabilities: {
        evaluation: issues.some((i) => i.code === 'UNKNOWN_STAT')
          ? 'UNKNOWN'
          : 'SUPPORTED',
        probability: 'UNSUPPORTED',
      },
    }
  },
}
