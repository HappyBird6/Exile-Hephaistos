import { useEffect, useRef, useState } from 'react'
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import type { Item } from './itemModels'
import { useI18n } from '../../shared/i18n/i18n'
import type { MessageKey } from '../../shared/i18n/i18n'
import { useItemDraft } from './draft'
import {
  ItemTextApiError,
  maxItemTextBytes,
  parseItemText,
} from './itemTextApi'

function errorKey(error: Error): MessageKey {
  if (error instanceof ItemTextApiError) {
    if (error.code === 'INVALID_ITEM_TEXT') return 'input.invalid'
    if (error.code === 'ITEM_TEXT_TOO_LARGE') return 'input.too_large'
    if (error.code === 'MALFORMED_REQUEST') return 'input.malformed'
  }
  return 'input.connection'
}

// Server results stay in TanStack Query; only input lives in the draft store.
export function useItemTextImport() {
  const { t } = useI18n()
  const client = useQueryClient()
  const currentKey = ['workbench', 'current-item'] as const
  const current = useQuery<Item>({
    queryKey: currentKey,
    queryFn: skipToken,
    gcTime: Infinity,
  })
  const [revision, setRevision] = useState(0)
  const currentRevision = useRef(0)
  const controller = useRef<AbortController | null>(null)
  const [validation, setValidation] = useState<MessageKey | null>(null)
  const mutation = useMutation({
    gcTime: 0,
    retry: false,
    mutationFn: async (input: {
      text: string
      revision: number
      signal: AbortSignal
    }) => ({
      item: await parseItemText(input.text, input.signal),
      revision: input.revision,
    }),
  })
  useEffect(
    () => () => {
      currentRevision.current += 1
      controller.current?.abort()
    },
    [],
  )
  function invalidate() {
    controller.current?.abort()
    currentRevision.current += 1
    setRevision(currentRevision.current)
    mutation.reset()
    setValidation(null)
    return currentRevision.current
  }
  async function submit(text: string, canAccept: () => boolean = () => true) {
    const original = useItemDraft.getState()
    const nextRevision = invalidate()
    if (!text.trim()) {
      setValidation('input.empty')
      return false
    }
    if (new TextEncoder().encode(text).length > maxItemTextBytes) {
      setValidation('input.too_large')
      return false
    }
    const request = new AbortController()
    controller.current = request
    try {
      const result = await mutation.mutateAsync({
        text,
        revision: nextRevision,
        signal: request.signal,
      })
      if (
        !canAccept() ||
        currentRevision.current !== nextRevision ||
        request.signal.aborted ||
        useItemDraft.getState().baseRevision !== original.baseRevision ||
        useItemDraft.getState().text !== text
      )
        return false
      // Every accepted server replacement updates the card and its text together.
      client.setQueryData(currentKey, result.item)
      useItemDraft.getState().acceptText(result.item.text.originalText)
      return true
    } catch {
      return false
    }
  }
  const isCurrent = mutation.variables?.revision === revision
  return {
    submit,
    invalidate,
    data: current.data,
    pending: isCurrent && mutation.isPending,
    error: validation
      ? t(validation)
      : isCurrent && mutation.error
        ? t(errorKey(mutation.error))
        : null,
  }
}
