import { Summary } from './entity/Summary';
import { Skill } from './entity/Skill';
import { Education } from './entity/Education';
import { Experience } from './entity/Experience';
import { Certification } from './entity/Certification';
import { Personal } from './entity/Personal';
import { Project } from './entity/Project';
import { Publication } from './entity/Publication';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

/**
 * Read access to the resume data served by askpranav-service (the same backend the chat talks to).
 * The base URL comes from the environment file, so nothing here points at a fixed host.
 */
@Injectable({
  providedIn: 'root'
})
export class BiographyServiceService {

  constructor(private http: HttpClient) { }

  private url(path: string): string {
    return environment.askpranavApiUrl + path;
  }

  public getSummaryDetails(): Observable<Summary> {
    return this.http.get<Summary>(this.url('/summary/summary-details'));
  }

  public getSkills(): Observable<Skill[]> {
    return this.http.get<Skill[]>(this.url('/skill/skill-details'));
  }

  public getSkillsByType(type: string): Observable<Skill[]> {
    return this.http.get<Skill[]>(this.url('/skill/skill-details/by-type'), { params: { type: type } });
  }

  public getPersonalDetails(): Observable<Personal> {
    return this.http.get<Personal>(this.url('/personal/personal-details'));
  }

  public getEducationDetails(): Observable<Education[]> {
    return this.http.get<Education[]>(this.url('/education/education-details'));
  }

  public getExperienceDetails(): Observable<Experience[]> {
    return this.http.get<Experience[]>(this.url('/experience/experience-details'));
  }

  public getCertificationList(): Observable<Certification[]> {
    return this.http.get<Certification[]>(this.url('/certification/certification-details'));
  }

  public getProjectDetails(): Observable<Project[]> {
    return this.http.get<Project[]>(this.url('/project/project-details'));
  }

  public getPublicationDetails(): Observable<Publication[]> {
    return this.http.get<Publication[]>(this.url('/publication/publication-details'));
  }
}
