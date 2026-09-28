export interface AskSource {
  type: string;
  label: string;
  excerpt: string;
}

/** Response of POST /ask/question on the AskPranav backend. */
export interface AskAnswer {
  answer: string;
  sources: AskSource[];
  planUsed: string;
}
