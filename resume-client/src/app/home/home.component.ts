import { BiographyServiceService } from './../biography-service.service';
import { Summary } from './../entity/Summary';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

/** A run of summary text; `hl` marks a key term that is visually highlighted. */
export interface SummaryPart {
  text: string;
  hl: boolean;
}

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  // Terms highlighted inside the summary. The text itself is never changed, only wrapped.
  private static readonly KEYWORDS: string[] = [
    'more than 6 years', 'Java', 'Spring Framework', 'Spring Boot', 'Microservices', 'REST APIs',
    'Telecom', 'Automotive', 'Payments/Fintech', 'Banking and Financial', 'Maps/Navigation',
    'SDLC', 'AWS', 'Docker', 'Kubernetes', 'CI/CD', 'Agile', 'English (C1)', 'German (A2)'
  ];

  summaryDetails: Summary;
  /** The summary as sentences, each split into plain and highlighted parts, for the staggered reveal. */
  sentences: SummaryPart[][] = [];
  error = false;

  constructor(private biographyServiceService: BiographyServiceService,
    private router: Router) { }

  ngOnInit(): void {
    this.getSummaryDetails();
  }

  routeToContactMe() {
    this.router.navigateByUrl('/contact-me');
  }

  routeToAsk() {
    this.router.navigateByUrl('/ask');
  }

  getSummaryDetails() {
    this.biographyServiceService.getSummaryDetails().subscribe(
      (data) => {
        this.summaryDetails = data;
        this.sentences = this.toSentences(data && data.summaryDetails);
      },
      () => this.error = true
    );
  }

  toSentences(text: string): SummaryPart[][] {
    if (!text) {
      return [];
    }
    const pattern = new RegExp('(' + HomeComponent.KEYWORDS
      .slice()
      .sort((a, b) => b.length - a.length) // longest first, so "Spring Framework" wins over a shorter overlap
      .map(k => k.replace(/[.*+?^${}()|[\]\\\/]/g, '\\$&'))
      .join('|') + ')', 'g');

    return (text.match(/[^.]+\.?/g) || [text])
      .map(sentence => sentence.trim())
      .filter(sentence => sentence.length > 0)
      .map(sentence => sentence
        .split(pattern)
        .filter(part => part.length > 0)
        .map(part => ({ text: part, hl: HomeComponent.KEYWORDS.indexOf(part) >= 0 })));
  }

}
