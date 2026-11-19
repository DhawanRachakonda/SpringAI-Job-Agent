import { useEffect, useRef, useState } from "react";

const WELCOME = {
  role: "bot",
  text: "Paste a job description. I'll extract the details, ask for anything missing, then check, score and publish it.",
};

export default function App() {
  const [messages, setMessages] = useState([WELCOME]);
  const [input, setInput] = useState("");
  const [threadId, setThreadId] = useState(null); // set while the pipeline is paused on a question
  const [busy, setBusy] = useState(false);
  const bottom = useRef(null);

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, busy]);

  async function send(e) {
    e.preventDefault();
    const message = input.trim();
    if (!message || busy) return;

    setMessages((m) => [...m, { role: "user", text: message }]);
    setInput("");
    setBusy(true);
    try {
      const res = await fetch("/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message, thread_id: threadId }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
      setMessages((m) => [...m, { role: "bot", text: data.reply }]);
      setThreadId(data.done ? null : data.thread_id);
    } catch (err) {
      setMessages((m) => [...m, { role: "bot", text: `Error: ${err.message}`, error: true }]);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="app">
      <header>
        <h1>JD Intake Assistant</h1>
        <p>{threadId ? "Answering a question…" : "Ready for a new job description"}</p>
      </header>

      <section className="chat" aria-live="polite">
        {messages.map((m, i) => (
          <div key={i} className={`msg ${m.role}${m.error ? " error" : ""}`}>
            {m.text}
          </div>
        ))}
        {busy && <div className="msg bot muted">Working…</div>}
        <div ref={bottom} />
      </section>

      <form onSubmit={send}>
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) send(e);
          }}
          placeholder={threadId ? "Type your answer…" : "Paste a job description…"}
          rows={3}
          disabled={busy}
        />
        <button type="submit" disabled={busy || !input.trim()}>
          {threadId ? "Answer" : "Submit"}
        </button>
      </form>
    </main>
  );
}
