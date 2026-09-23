import { Summary } from './entity/Summary';
import { Skill } from './entity/Skill';
import { Education } from './entity/Education';
import { Experience } from './entity/Experience';
import { Certification } from './entity/Certification';
import { Personal } from './entity/Personal';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class BiographyServiceService {

  biographyServiceBaseURL = "http://localhost:8080";
  biographyServiceAWSBaseURL = "http://pranavresumesite-env.eba-mdmqjdmy.us-east-1.elasticbeanstalk.com";

  constructor(private http:HttpClient) { }

  public getSummaryDetails() : Observable<Summary>{
    return this.http.get<Summary>(this.biographyServiceAWSBaseURL+"/summary/summary-details");
  }

  public getSkillByType(type):Observable<Skill>{
    return this.http.get<Skill>(this.biographyServiceAWSBaseURL + "/skill/skill-details/type/" + type);
  }

  public getPersonalDetails(): Observable<Personal>{
    return this.http.get<Personal>(this.biographyServiceAWSBaseURL+"/personal/personal-details");
  }
  
  public getEducationDetails() : Observable<Education>{
    return this.http.get<Education>(this.biographyServiceAWSBaseURL+"/education/education-details");
  }
  
  public getExperienceDetails(): Observable<Experience> {
    return this.http.get<Experience>(this.biographyServiceAWSBaseURL+"/experience/experience-details");
  }
  
  public getCertificationList() : Observable<Certification[]>{
    return this.http.get<Certification[]>(this.biographyServiceAWSBaseURL+"/certification/certification-details");
  }
  
}
