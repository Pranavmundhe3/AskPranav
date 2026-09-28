import { Component } from '@angular/core';
import { NgForm } from '@angular/forms';
import { ContactPayload, ContactService } from '../contact.service';

@Component({
  selector: 'app-contact-me',
  templateUrl: './contact-me.component.html',
  styleUrls: ['./contact-me.component.css']
})
export class ContactMeComponent {

  model: ContactPayload = { name: '', email: '', message: '', website: '' };
  sending = false;
  success: string = null;
  failure: string = null;

  constructor(private contactService: ContactService) { }

  submit(form: NgForm): void {
    if (form.invalid || this.sending) {
      return;
    }
    this.sending = true;
    this.success = null;
    this.failure = null;

    this.contactService.send(this.model).subscribe(
      (result) => {
        this.success = result.message;
        this.sending = false;
        form.resetForm({ name: '', email: '', message: '', website: '' });
      },
      (err) => {
        // The backend explains rate limits and mail problems in `message`.
        this.failure = (err && err.error && err.error.message)
          || (err && err.status === 0 ? 'Could not reach the server. Please try again later.' : 'Something went wrong. Please try again.');
        this.sending = false;
      }
    );
  }
}
