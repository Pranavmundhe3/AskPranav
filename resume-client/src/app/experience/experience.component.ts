import { Certification } from './../entity/Certification';
import { Experience } from './../entity/Experience';
import { Component, OnInit } from '@angular/core';
import { BiographyServiceService } from './../biography-service.service';

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

  // Front-end only: the backend has no logo field, and these three companies are unlikely to change
  // often. A company not listed here just shows no logo instead of a broken image.
  private static readonly LOGOS: { [companyMatch: string]: string } = {
    't-systems': 'assets/logos/t-systems.png',
    'here technologies': 'assets/logos/here-technologies.png',
    'ltimindtree': 'assets/logos/ltimindtree.svg'
  };

  /** The backend stores one bullet point per line; blank lines are ignored. */
  bulletsOf(description: string): string[] {
    return (description || '').split('\n').map(line => line.trim()).filter(line => line.length > 0);
  }

  logoFor(company: string): string {
    const lower = (company || '').toLowerCase();
    const key = Object.keys(ExperienceComponent.LOGOS).find(k => lower.includes(k));
    return key ? ExperienceComponent.LOGOS[key] : null;
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
