import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { SessionTitleEditor } from './SessionTitleEditor'
function begin(save = vi.fn(() => true)) {
  const view = render(
    <SessionTitleEditor title="Original title" onSave={save} />,
  )
  const heading = screen.getByRole('heading', { name: 'Original title' })
  fireEvent.click(
    screen.getByRole('button', { name: 'Rename: Original title' }),
  )
  const editor = screen.getByRole('textbox', { name: 'Session title' })
  expect(editor).toBe(heading)
  expect(view.container.querySelector('input')).toBeNull()
  return { editor, save, view }
}
it('edits the existing title in place and saves only once for Enter followed by blur', () => {
  const { editor, save } = begin()
  expect(editor).toHaveTextContent('Original title')
  editor.textContent = 'Updated title'
  fireEvent.keyDown(editor, { key: 'Enter' })
  fireEvent.blur(editor)
  expect(save).toHaveBeenCalledExactlyOnceWith('Updated title')
})
it('saves on blur, cancels with Escape and does not submit during IME composition', () => {
  const { editor, save } = begin()
  editor.textContent = '日本語'
  fireEvent.compositionStart(editor)
  fireEvent.keyDown(editor, { key: 'Enter', isComposing: true })
  expect(save).not.toHaveBeenCalled()
  fireEvent.compositionEnd(editor)
  fireEvent.keyDown(editor, { key: 'Escape' })
  fireEvent.blur(editor)
  expect(save).not.toHaveBeenCalled()
  expect(screen.getByRole('heading', { name: 'Original title' })).toBeVisible()
  fireEvent.click(
    screen.getByRole('button', { name: 'Rename: Original title' }),
  )
  const next = screen.getByRole('textbox', { name: 'Session title' })
  next.textContent = 'Saved by blur'
  fireEvent.blur(next)
  expect(save).toHaveBeenCalledExactlyOnceWith('Saved by blur')
})
it('retains edits when persistence fails and supports retry', () => {
  const save = vi.fn().mockReturnValueOnce(false).mockReturnValueOnce(true)
  const { editor } = begin(save)
  editor.textContent = 'Keep this edit'
  fireEvent.keyDown(editor, { key: 'Enter' })
  expect(screen.getByRole('alert')).toBeVisible()
  expect(editor).toHaveAttribute('contenteditable', 'plaintext-only')
  expect(editor).toHaveTextContent('Keep this edit')
  fireEvent.keyDown(editor, { key: 'Enter' })
  expect(save).toHaveBeenCalledTimes(2)
})
it('keeps an unchanged generated name derived and prevents blank titles', () => {
  const { editor, save } = begin()
  fireEvent.keyDown(editor, { key: 'Enter' })
  expect(save).not.toHaveBeenCalled()
  fireEvent.click(
    screen.getByRole('button', { name: 'Rename: Original title' }),
  )
  const next = screen.getByRole('textbox', { name: 'Session title' })
  next.textContent = '   '
  fireEvent.blur(next)
  expect(save).not.toHaveBeenCalled()
  expect(screen.getByRole('heading', { name: 'Original title' })).toBeVisible()
})
