import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { environment } from '../environments/environment';
import { AskAnswer } from './entity/AskAnswer';

/** Talks to the AskPranav backend (askpranav-service), not the older biography endpoints. */
@Injectable({
  providedIn: 'root'
})
export class AskService {

  // Local models can take a while on a cold start, so allow a generous wait before giving up.
  private static readonly TIMEOUT_MS = 180000;

  constructor(private http: HttpClient) { }

  public ask(question: string): Observable<AskAnswer> {
    return this.http
      .post<AskAnswer>(environment.askpranavApiUrl + '/ask/question', { question: question })
      .pipe(timeout(AskService.TIMEOUT_MS));
  }
}
