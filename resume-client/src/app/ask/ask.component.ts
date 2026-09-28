import { Component, ElementRef, ViewChild } from '@angular/core';
import { AskService } from '../ask.service';
import { AskAnswer, AskSource } from '../entity/AskAnswer';

export interface Segment {
  text: string;
  bold: boolean;
}

/** A paragraph (one line) or a bullet list (one line per item). */
export interface Block {
  list: boolean;
  lines: Segment[][];
}

export interface ChatMessage {
  role: 'user' | 'bot';
  text: string;
  blocks: Block[];
  sources: AskSource[];
  error: boolean;
  loading: boolean;
}

@Component({
  selector: 'app-ask',
  templateUrl: './ask.component.html',
  styleUrls: ['./ask.component.css']
})
export class AskComponent {

  @ViewChild('log') log: ElementRef;

  readonly suggestions: string[] = [
    'Give me a 30-second summary of Pranav',
    'What is his experience with RabbitMQ and event-driven systems?',
    'Has he worked in fintech or payments?',
    'What certifications does he have?',
    'What are his contact details?'
  ];

  messages: ChatMessage[] = [
    this.botMessage('Hi, I\'m AskPranav. Ask me anything about Pranav\'s career, or paste a job description ' +
      'and I\'ll show how his experience maps to it.')
  ];

  draft = '';
  sending = false;

  constructor(private askService: AskService) { }

  /** Sends the given question (a suggestion chip) or, if none is given, the text typed in the box. */
  send(question?: string): void {
    const text = (question !== undefined ? question : this.draft).trim();
    if (!text || this.sending) {
      return;
    }
    this.draft = '';
    this.sending = true;
    this.messages.push(this.userMessage(text));
    const reply = this.loadingMessage();
    this.messages.push(reply);
    this.scrollToBottom();

    this.askService.ask(text).subscribe(
      (data: AskAnswer) => {
        this.fill(reply, data.answer, data.sources, false);
        this.sending = false;
      },
      (err) => {
        this.fill(reply, this.errorText(err), [], true);
        this.sending = false;
      }
    );
  }

  onEnter(event: Event): void {
    // Plain Enter sends; Shift+Enter is not matched by (keydown.enter) and still inserts a newline.
    event.preventDefault();
    this.send();
  }

  /** Turns answer text into blocks. Only **bold** and "* " / "- " bullets are recognised, and the
   *  template renders everything through interpolation, so model output can never inject HTML. */
  toBlocks(text: string): Block[] {
    const blocks: Block[] = [];
    let list: Block = null;
    for (const line of (text || '').split('\n')) {
      const bullet = line.match(/^\s*[*\-•]\s+(.*)$/);
      if (bullet) {
        if (!list) {
          list = { list: true, lines: [] };
          blocks.push(list);
        }
        list.lines.push(this.toSegments(bullet[1]));
      } else if (line.trim() === '') {
        list = null;
      } else {
        list = null;
        blocks.push({ list: false, lines: [this.toSegments(line.trim())] });
      }
    }
    return blocks;
  }

  private toSegments(line: string): Segment[] {
    return line
      .split(/(\*\*[^*]+\*\*)/g)
      .filter(part => part.length > 0)
      .map(part => /^\*\*[^*]+\*\*$/.test(part)
        ? { text: part.slice(2, -2), bold: true }
        : { text: part, bold: false });
  }

  private fill(message: ChatMessage, text: string, sources: AskSource[], error: boolean): void {
    message.text = text;
    message.blocks = this.toBlocks(text);
    message.sources = sources || [];
    message.error = error;
    message.loading = false;
    this.scrollToBottom();
  }

  /** The backend explains rate limits (429) and model outages (503) in the body's `answer` field. */
  private errorText(err: any): string {
    if (err && err.error && typeof err.error.answer === 'string') {
      return err.error.answer;
    }
    if (err && err.name === 'TimeoutError') {
      return 'That took too long. Please try again.';
    }
    if (err && err.status === 0) {
      return 'Could not reach the AskPranav service. Please try again later.';
    }
    return 'Something went wrong. Please try again.';
  }

  private userMessage(text: string): ChatMessage {
    return { role: 'user', text: text, blocks: [], sources: [], error: false, loading: false };
  }

  private botMessage(text: string): ChatMessage {
    return { role: 'bot', text: text, blocks: this.toBlocks(text), sources: [], error: false, loading: false };
  }

  private loadingMessage(): ChatMessage {
    return { role: 'bot', text: '', blocks: [], sources: [], error: false, loading: true };
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const el = this.log && this.log.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }
}
