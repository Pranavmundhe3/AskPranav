import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ContactMeComponent } from './contact-me.component';
import { environment } from '../../environments/environment';

describe('ContactMeComponent', () => {
  let component: ContactMeComponent;
  let fixture: ComponentFixture<ContactMeComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ContactMeComponent ],
      imports: [ FormsModule, HttpClientTestingModule ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ContactMeComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('keeps the submit button disabled until the form is valid', async () => {
    await fixture.whenStable();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type=submit]');
    expect(button.disabled).toBe(true);
  });

  it('shows the backend explanation when sending fails', () => {
    component.model = { name: 'Ada', email: 'ada@example.com', message: 'Hello', website: '' };
    const form: any = { invalid: false, resetForm: () => {} };

    component.submit(form);
    http.expectOne(environment.askpranavApiUrl + '/contact/send')
      .flush({ message: 'The contact form is not configured yet.' }, { status: 503, statusText: 'Service Unavailable' });

    expect(component.failure).toBe('The contact form is not configured yet.');
    expect(component.sending).toBe(false);
  });
});
