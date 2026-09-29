import { Certification } from './../entity/Certification';
import { Experience } from './../entity/Experience';
import { Component, OnInit } from '@angular/core';
import { BiographyServiceService } from './../biography-service.service';
import { logoForCompany } from './../shared/company-logos';

@Component({
  selector: 'app-experience',
  templateUrl: './experience.component.html',
  styleUrls: ['./experience.component.css']
})
export class ExperienceComponent implements OnInit {

  expDetails: Experience[] = [];
  certDetails: Certification[] = [];
  error = false;

  constructor(private biographyServiceService: BiographyServiceService) { }

  ngOnInit(): void {
    this.getExpDetails();
    this.getCertDetails();
  }

  /** The backend stores one bullet point per line; blank lines are ignored. */
  bulletsOf(description: string): string[] {
    return (description || '').split('\n').map(line => line.trim()).filter(line => line.length > 0);
  }

  logoFor(company: string): string {
    return logoForCompany(company);
  }

  getExpDetails() {
    this.biographyServiceService.getExperienceDetails().subscribe(
      (data) => this.expDetails = data,
      () => this.error = true
    );
  }

  getCertDetails() {
    this.biographyServiceService.getCertificationList().subscribe(
      (data) => this.certDetails = data,
      () => this.error = true
    );
  }
}
