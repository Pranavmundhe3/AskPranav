import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AskComponent } from './ask.component';
import { environment } from '../../environments/environment';

describe('AskComponent', () => {
  let component: AskComponent;
  let fixture: ComponentFixture<AskComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AskComponent ],
      imports: [ FormsModule, HttpClientTestingModule ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AskComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should create and greet the visitor', () => {
    expect(component).toBeTruthy();
    expect(component.messages.length).toBe(1);
    expect(component.messages[0].role).toBe('bot');
  });

  it('splits bullets and bold text into blocks', () => {
    const blocks = component.toBlocks('Intro line\n\n* **Java**: Spring Boot\n* AWS');

    expect(blocks.length).toBe(2);
    expect(blocks[0].list).toBe(false);
    expect(blocks[1].list).toBe(true);
    expect(blocks[1].lines.length).toBe(2);
    expect(blocks[1].lines[0][0]).toEqual({ text: 'Java', bold: true });
  });

  it('posts the question and shows the answer', () => {
    component.send('What certifications does he have?');

    const req = http.expectOne(environment.askpranavApiUrl + '/ask/question');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ question: 'What certifications does he have?' });
    req.flush({ answer: 'He has **two** certifications.', sources: [], planUsed: 'TOOL_CALL' });

    const reply = component.messages[component.messages.length - 1];
    expect(reply.loading).toBe(false);
    expect(reply.error).toBe(false);
    expect(reply.text).toContain('two');
    expect(component.sending).toBe(false);
  });

  it('shows the backend explanation when rate limited', () => {
    component.send('One more question');

    const req = http.expectOne(environment.askpranavApiUrl + '/ask/question');
    req.flush({ answer: 'Please try again in 3 minute(s).', sources: [], planUsed: 'RATE_LIMITED' },
      { status: 429, statusText: 'Too Many Requests' });

    const reply = component.messages[component.messages.length - 1];
    expect(reply.error).toBe(true);
    expect(reply.text).toBe('Please try again in 3 minute(s).');
  });

  it('ignores blank questions', () => {
    component.send('   ');

    http.expectNone(environment.askpranavApiUrl + '/ask/question');
    expect(component.messages.length).toBe(1);
  });
});
