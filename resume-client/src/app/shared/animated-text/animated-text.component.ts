import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TextPart, toSentences } from '../text-animation';

/**
 * Renders `text` as staggered, sentence-by-sentence cards with `keywords` highlighted - the same
 * treatment Home uses for the summary, factored out so Projects/Publications can reuse it instead of
 * duplicating the splitting/highlighting logic.
 */
@Component({
  selector: 'app-animated-text',
  templateUrl: './animated-text.component.html',
  styleUrls: ['./animated-text.component.css']
})
export class AnimatedTextComponent implements OnChanges {

  @Input() text: string;
  @Input() keywords: string[] = [];
  /** Animation delay (ms) before the first sentence starts, so a parent can stagger this after its own entrance. */
  @Input() baseDelayMs = 0;
  /** Gap (ms) between each sentence's start. */
  @Input() stepMs = 350;

  sentences: TextPart[][] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.text || changes.keywords) {
      this.sentences = toSentences(this.text, this.keywords);
    }
  }
}
