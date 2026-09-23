import { useEffect, useRef, useState } from 'react'
import {
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import type { Item } from './itemModels'
import { useItemDraft } from './draft'
import type { MessageKey } from '../../shared/i18n/messages'
import {
  ItemTextApiError,
  maxItemTextBytes,
  parseItemText,
} from './itemTextApi'

function errorKey(error: Error): MessageKey {
  if (error instanceof ItemTextApiError) {
    if (error.code === 'INVALID_ITEM_TEXT') return 'errorInvalidItem'
    if (error.code === 'ITEM_TEXT_TOO_LARGE') return 'errorTooLarge'
    if (error.code === 'MALFORMED_REQUEST') return 'errorMalformed'
  }
  return 'errorNetwork'
}

// Server results stay in TanStack Query; only input lives in the draft store.
export function useItemTextImport() {
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
  async function submit(text: string) {
    const nextRevision = invalidate()
    if (!text.trim()) {
      setValidation('errorBlank')
      return false
    }
    if (new TextEncoder().encode(text).length > maxItemTextBytes) {
      setValidation('errorTooLarge')
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
      if (currentRevision.current !== nextRevision || request.signal.aborted)
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
    error:
      validation ??
      (isCurrent && mutation.error ? errorKey(mutation.error) : null),
  }
}
