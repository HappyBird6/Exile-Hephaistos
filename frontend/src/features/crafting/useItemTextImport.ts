import { useEffect, useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
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
  function submit(text: string) {
    const nextRevision = invalidate()
    if (!text.trim()) {
      setValidation('errorBlank')
      return
    }
    if (new TextEncoder().encode(text).length > maxItemTextBytes) {
      setValidation('errorTooLarge')
      return
    }
    const request = new AbortController()
    controller.current = request
    mutation.mutate({ text, revision: nextRevision, signal: request.signal })
  }
  const isCurrent = mutation.variables?.revision === revision
  return {
    submit,
    invalidate,
    data:
      isCurrent && mutation.data?.revision === revision
        ? mutation.data.item
        : undefined,
    pending: isCurrent && mutation.isPending,
    error:
      validation ??
      (isCurrent && mutation.error ? errorKey(mutation.error) : null),
  }
}
