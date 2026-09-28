import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface ContactPayload {
  name: string;
  email: string;
  message: string;
  /** Honeypot: hidden from people, so anything in here means a bot filled the form. */
  website: string;
}

export interface ContactResult {
  message: string;
}

/** Sends the contact form to the backend, which emails the message to Pranav. */
@Injectable({
  providedIn: 'root'
})
export class ContactService {

  constructor(private http: HttpClient) { }

  public send(payload: ContactPayload): Observable<ContactResult> {
    return this.http.post<ContactResult>(environment.askpranavApiUrl + '/contact/send', payload);
  }
}
