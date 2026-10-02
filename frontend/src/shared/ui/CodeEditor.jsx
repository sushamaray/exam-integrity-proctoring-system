import { useMemo } from 'react'
import CodeMirror from '@uiw/react-codemirror'
import { acceptCompletion, autocompletion, completionStatus } from '@codemirror/autocomplete'
import { indentWithTab } from '@codemirror/commands'
import { cpp } from '@codemirror/lang-cpp'
import { python } from '@codemirror/lang-python'
import { HighlightStyle, indentUnit, syntaxHighlighting } from '@codemirror/language'
import { Prec } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'
import { tags as t } from '@lezer/highlight'
import { completionsFor } from '../lib/completions.js'

const ink = (pct) => `color-mix(in srgb, var(--color-ink) ${pct}%, transparent)`
const navy = (pct) => `color-mix(in srgb, var(--color-navy) ${pct}%, transparent)`

// Built from the @theme tokens in index.css rather than a stock theme, so the editor reads as
// part of the console instead of a widget dropped into it.
const surface = EditorView.theme({
  '&': {
    fontSize: '13px',
    backgroundColor: 'var(--color-surface)',
    color: 'var(--color-ink)',
    height: '100%',
  },
  '&.cm-editor.cm-focused': { outline: 'none' },
  '.cm-scroller': { fontFamily: 'var(--font-mono)', lineHeight: '1.65' },
  '.cm-content': { padding: '12px 0', caretColor: 'var(--color-navy)' },
  '.cm-gutters': {
    backgroundColor: 'var(--color-paper)',
    color: ink(38),
    border: 'none',
    borderRight: `1px solid ${navy(10)}`,
  },
  '.cm-activeLine': { backgroundColor: navy(4) },
  '.cm-activeLineGutter': { backgroundColor: navy(7), color: 'var(--color-navy-dark)' },
  '.cm-selectionBackground, &.cm-focused .cm-selectionBackground': { backgroundColor: navy(16) },
  '.cm-cursor': { borderLeftColor: 'var(--color-navy)', borderLeftWidth: '2px' },
  '.cm-matchingBracket, &.cm-focused .cm-matchingBracket': {
    backgroundColor: navy(14),
    outline: 'none',
  },
  // The completion popup ships unstyled, which in a palette this restrained would look like a
  // stock widget pasted over the editor.
  '.cm-tooltip.cm-tooltip-autocomplete': {
    backgroundColor: 'var(--color-paper)',
    border: `1px solid ${navy(14)}`,
    borderRadius: '6px',
    boxShadow: `0 6px 18px ${navy(10)}`,
    overflow: 'hidden',
  },
  '.cm-tooltip-autocomplete > ul': { fontFamily: 'var(--font-mono)', maxHeight: '15em' },
  '.cm-tooltip-autocomplete > ul > li': { padding: '3px 8px', color: 'var(--color-ink)' },
  '.cm-tooltip-autocomplete > ul > li[aria-selected]': {
    backgroundColor: navy(16),
    color: 'var(--color-navy-dark)',
  },
  '.cm-completionLabel': { fontWeight: '500' },
  '.cm-completionMatchedText': { textDecoration: 'none', color: 'var(--color-navy-dark)' },
  '.cm-completionDetail': { fontStyle: 'normal', color: ink(45), marginLeft: '0.75em' },
})

// The palette is deliberately hue-free everywhere else in the app, so highlighting leans on
// navy depth, weight and italics rather than the usual rainbow.
const highlight = HighlightStyle.define([
  { tag: [t.keyword, t.controlKeyword, t.modifier], color: 'var(--color-navy-dark)', fontWeight: '500' },
  { tag: [t.meta, t.processingInstruction], color: ink(55) },
  { tag: [t.typeName, t.standard(t.typeName)], color: 'var(--color-navy)' },
  { tag: [t.string, t.character, t.special(t.string)], color: 'var(--color-navy)' },
  { tag: [t.number, t.bool, t.null], color: 'var(--color-navy)', fontWeight: '500' },
  { tag: [t.comment, t.lineComment, t.blockComment], color: ink(42), fontStyle: 'italic' },
  { tag: [t.function(t.variableName), t.function(t.propertyName)], color: 'var(--color-ink)', fontWeight: '500' },
  { tag: [t.operator, t.punctuation, t.bracket], color: ink(66) },
  { tag: t.invalid, color: 'var(--color-navy-dark)', textDecoration: 'underline wavy' },
])

/** C++'s grammar covers C, so one parser serves both. */
const SUPPORT = { C: cpp, CPP: cpp, PYTHON: python }

/**
 * Pressing Tab in the editor means "get on with it": take the completion if one is offered,
 * otherwise indent.
 *
 * `acceptCompletion` answers false when no popup is open, which lets the same key fall through to
 * `indentWithTab` -- so one binding does both jobs, and Shift-Tab still outdents.
 *
 * Escape is the way back out. Binding Tab costs a keyboard user the usual way of leaving a
 * focused editor, so Escape blurs it -- after the popup's own Escape handler has had its turn,
 * which is why this checks for one first.
 */
const tabKeys = Prec.highest(keymap.of([
  { key: 'Tab', run: acceptCompletion },
  indentWithTab,
  {
    key: 'Escape',
    run: (view) => {
      if (completionStatus(view.state) != null) return false
      view.contentDOM.blur()
      return true
    },
  },
]))

/**
 * The exam code editor.
 *
 * Tab completes from a fixed list per language -- keywords, the standard library an intro course
 * uses, snippets, and the identifiers already in the file. See shared/lib/completions.js for why
 * it is a list and not a language server.
 */
export default function CodeEditor({ value, onChange, language = 'C', readOnly = false, className = '' }) {
  const extensions = useMemo(() => {
    const support = (SUPPORT[language] ?? cpp)()
    const base = [support, surface, syntaxHighlighting(highlight), indentUnit.of('    ')]
    // Nothing to complete or indent in a read-only view, and capturing Tab there would only
    // trap a keyboard user in a panel they cannot edit.
    if (readOnly) return base
    return [
      ...base,
      support.language.data.of({ autocomplete: completionsFor(language) }),
      autocompletion({ activateOnTyping: true, icons: false, closeOnBlur: true }),
      tabKeys,
    ]
  }, [language, readOnly])

  return (
    <div className={`overflow-hidden rounded-xl border border-navy/14 bg-surface ${className}`}>
      <CodeMirror
        value={value}
        onChange={onChange}
        readOnly={readOnly}
        height="100%"
        theme="none"
        extensions={extensions}
        basicSetup={{
          lineNumbers: true,
          foldGutter: false,
          // Our own autocompletion extension is in `extensions`, configured and themed there.
          autocompletion: false,
          highlightActiveLine: !readOnly,
          highlightSelectionMatches: false,
          bracketMatching: true,
          closeBrackets: true,
          tabSize: 4,
        }}
      />
    </div>
  )
}
