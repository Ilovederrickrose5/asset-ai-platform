/**
 * 解析 SSE 缓冲区中的完整事件。
 *
 * Spring 会把含换行的文本编码成多行 `data:`，同一事件内的多行需要用 `\n`
 * 重新拼接，否则 Markdown 的标题和列表会丢失行语义。
 *
 * @param {string} buffer 累积的原始文本（可能包含不完整事件）
 * @returns {{events: Array<{event: string, data: string}>, rest: string}}
 */
export function parseSseEvents(buffer) {
  const parts = buffer.split(/\r?\n\r?\n/)
  const rest = parts.pop() ?? ''
  const events = []

  for (const rawEvent of parts) {
    let eventName = 'message'
    const dataLines = []

    for (const line of rawEvent.split(/\r?\n/)) {
      if (!line || line.startsWith(':')) continue
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim() || 'message'
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).replace(/^ /, ''))
      }
    }

    if (!dataLines.length) continue
    events.push({ event: eventName, data: dataLines.join('\n') })
  }

  return { events, rest }
}
