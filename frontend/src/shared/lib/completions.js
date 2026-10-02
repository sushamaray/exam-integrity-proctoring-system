import { completeAnyWord, completeFromList, snippetCompletion } from '@codemirror/autocomplete'

/**
 * What the editor offers when you press Tab.
 *
 * Deliberately a fixed list rather than a language server: the exam decides what help a student
 * gets, so the surface is one we chose -- keywords, the standard library an intro course actually
 * uses, and snippets for the shapes nobody should have to retype -- plus whatever identifiers are
 * already in the file, which is where variable and function names come from.
 */

const keywords = (names) => names.map((label) => ({ label, type: 'keyword' }))
// No `apply` that adds the open paren: inserting one would leave it unbalanced, and closeBrackets
// pairs it properly the moment the student types it themselves.
const calls = (entries) =>
  entries.map(([label, detail]) => ({ label, type: 'function', detail }))

const C_KEYWORDS = keywords([
  'auto', 'break', 'case', 'char', 'const', 'continue', 'default', 'do', 'double', 'else',
  'enum', 'extern', 'float', 'for', 'goto', 'if', 'int', 'long', 'return', 'short', 'signed',
  'sizeof', 'static', 'struct', 'switch', 'typedef', 'union', 'unsigned', 'void', 'volatile',
  'while', 'size_t', 'NULL',
])

const C_CALLS = calls([
  ['printf', 'write a formatted line'],
  ['scanf', 'read formatted input'],
  ['puts', 'write a line'],
  ['putchar', 'write one character'],
  ['getchar', 'read one character'],
  ['fgets', 'read a line into a buffer'],
  ['sprintf', 'format into a string'],
  ['sscanf', 'read formatted input from a string'],
  ['malloc', 'allocate memory'],
  ['calloc', 'allocate zeroed memory'],
  ['realloc', 'resize an allocation'],
  ['free', 'release memory'],
  ['atoi', 'string to int'],
  ['atof', 'string to double'],
  ['abs', 'absolute value of an int'],
  ['rand', 'pseudo-random int'],
  ['srand', 'seed the generator'],
  ['exit', 'end the program'],
  ['qsort', 'sort an array'],
  ['strlen', 'length of a string'],
  ['strcpy', 'copy a string'],
  ['strncpy', 'copy at most n characters'],
  ['strcmp', 'compare two strings'],
  ['strncmp', 'compare at most n characters'],
  ['strcat', 'append a string'],
  ['strstr', 'find a substring'],
  ['strchr', 'find a character'],
  ['memset', 'fill memory'],
  ['memcpy', 'copy memory'],
  ['sqrt', 'square root'],
  ['pow', 'raise to a power'],
  ['fabs', 'absolute value of a double'],
  ['floor', 'round down'],
  ['ceil', 'round up'],
  ['round', 'round to nearest'],
  ['log', 'natural logarithm'],
  ['log10', 'base 10 logarithm'],
  ['fmax', 'larger of two doubles'],
  ['fmin', 'smaller of two doubles'],
])

const CPP_KEYWORDS = keywords([
  'bool', 'class', 'public', 'private', 'protected', 'virtual', 'override', 'template',
  'typename', 'namespace', 'using', 'new', 'delete', 'nullptr', 'true', 'false', 'try', 'catch',
  'throw', 'constexpr', 'explicit', 'friend', 'inline', 'operator', 'this',
])

const CPP_NAMES = [
  ['cout', 'std, write to output'],
  ['cin', 'std, read from input'],
  ['cerr', 'std, write to errors'],
  ['endl', 'std, newline and flush'],
  ['string', 'std'],
  ['vector', 'std'],
  ['map', 'std'],
  ['unordered_map', 'std'],
  ['set', 'std'],
  ['pair', 'std'],
  ['make_pair', 'std'],
  ['sort', 'std, sort a range'],
  ['reverse', 'std, reverse a range'],
  ['find', 'std, search a range'],
  ['accumulate', 'std, sum a range'],
  ['max', 'std'],
  ['min', 'std'],
  ['swap', 'std'],
  ['to_string', 'std'],
  ['stoi', 'std, string to int'],
  ['push_back', 'add to the end'],
  ['pop_back', 'remove from the end'],
  ['size', 'how many'],
  ['empty', 'whether there are none'],
  ['begin', 'first position'],
  ['end', 'one past the last'],
  ['substr', 'part of a string'],
  ['length', 'how many characters'],
].map(([label, detail]) => ({ label, type: 'variable', detail }))

// Without def, for, while, if, class, import or try: lang-python ships snippets under exactly
// those labels, and two sources offering the same word lists it twice.
const PYTHON_KEYWORDS = keywords([
  'and', 'as', 'assert', 'break', 'continue', 'del', 'elif', 'else', 'except', 'False',
  'finally', 'from', 'global', 'in', 'is', 'lambda', 'None', 'not', 'or', 'pass', 'raise',
  'return', 'True', 'with', 'yield',
])

/**
 * `${}` is a tab stop, so pressing Tab again walks the holes in order. The `\\n` sequences are a
 * literal backslash and n: they belong to the code being written, not to this file.
 */
const C_SNIPPETS = [
  snippetCompletion('int main(void) {\n    ${}\n    return 0;\n}', {
    label: 'main', type: 'keyword', detail: 'program entry point',
  }),
  snippetCompletion('#include <${stdio.h}>', { label: 'include', type: 'keyword', detail: 'a header' }),
  snippetCompletion('for (int ${i} = 0; ${i} < ${n}; ${i}++) {\n    ${}\n}', {
    label: 'for', type: 'keyword', detail: 'counting loop',
  }),
  snippetCompletion('while (${condition}) {\n    ${}\n}', { label: 'while', type: 'keyword' }),
  snippetCompletion('if (${condition}) {\n    ${}\n}', { label: 'if', type: 'keyword' }),
  snippetCompletion('if (${condition}) {\n    ${}\n} else {\n    \n}', {
    label: 'ifelse', type: 'keyword', detail: 'if with an else',
  }),
  snippetCompletion('printf("${%d}\\n", ${value});', {
    label: 'printf', type: 'function', detail: 'print one value',
  }),
  snippetCompletion('scanf("%d", &${n});', {
    label: 'scanf', type: 'function', detail: 'read one int',
  }),
  snippetCompletion('struct ${Name} {\n    ${}\n};', { label: 'struct', type: 'keyword' }),
]

const CPP_SNIPPETS = [
  snippetCompletion('int main() {\n    ${}\n    return 0;\n}', {
    label: 'main', type: 'keyword', detail: 'program entry point',
  }),
  snippetCompletion('#include <${iostream}>', { label: 'include', type: 'keyword', detail: 'a header' }),
  snippetCompletion('using namespace std;', {
    label: 'usingstd', type: 'keyword', detail: 'drop the std:: prefix',
  }),
  snippetCompletion('std::cout << ${} << std::endl;', {
    label: 'cout', type: 'function', detail: 'print a value',
  }),
  snippetCompletion('std::cin >> ${n};', { label: 'cin', type: 'function', detail: 'read a value' }),
  snippetCompletion('for (int ${i} = 0; ${i} < ${n}; ${i}++) {\n    ${}\n}', {
    label: 'for', type: 'keyword', detail: 'counting loop',
  }),
  snippetCompletion('for (auto& ${item} : ${items}) {\n    ${}\n}', {
    label: 'foreach', type: 'keyword', detail: 'loop over a container',
  }),
  snippetCompletion('while (${condition}) {\n    ${}\n}', { label: 'while', type: 'keyword' }),
  snippetCompletion('if (${condition}) {\n    ${}\n}', { label: 'if', type: 'keyword' }),
  snippetCompletion('std::vector<${int}> ${name};', { label: 'vector', type: 'keyword' }),
]

/**
 * Only what lang-python does not already offer. It ships def, for, while, if, try, class and
 * import, so what is left is the exam-shaped half: reading input, which every question starts
 * with, and the counting loop its own sequence `for` does not cover.
 */
const PYTHON_SNIPPETS = [
  snippetCompletion('for ${i} in range(${n}):\n    ${}', {
    label: 'forrange', type: 'keyword', detail: 'counting loop',
  }),
  snippetCompletion('${n} = int(input())', {
    label: 'readint', type: 'function', detail: 'read one number',
  }),
  snippetCompletion('${nums} = [int(x) for x in input().split()]', {
    label: 'readints', type: 'function', detail: 'read a line of numbers',
  }),
  snippetCompletion('if __name__ == "__main__":\n    ${}', {
    label: 'ifmain', type: 'keyword', detail: 'run only when started directly',
  }),
]

const BY_LANGUAGE = {
  C: [...C_SNIPPETS, ...C_KEYWORDS, ...C_CALLS],
  CPP: [...CPP_SNIPPETS, ...CPP_KEYWORDS, ...C_KEYWORDS, ...CPP_NAMES, ...C_CALLS],
  PYTHON: [...PYTHON_SNIPPETS, ...PYTHON_KEYWORDS],
}

/**
 * One source per language, merging the fixed list with the words already in the file.
 *
 * They are merged here rather than registered as two sources so a name that is in both -- every
 * keyword the student has already typed once -- appears once rather than twice.
 */
export function completionsFor(language) {
  const fromList = completeFromList(BY_LANGUAGE[language] ?? BY_LANGUAGE.C)

  return (context) => {
    const listed = fromList(context)
    const typed = completeAnyWord(context)
    if (!listed) return typed
    if (!typed) return listed

    const known = new Set(listed.options.map((option) => option.label))
    return {
      ...listed,
      options: [...listed.options, ...typed.options.filter((option) => !known.has(option.label))],
    }
  }
}
