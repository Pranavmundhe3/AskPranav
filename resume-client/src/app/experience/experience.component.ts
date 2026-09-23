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

  expDetails: Experience;
  certDetails: Certification[];

  constructor(private biographyServiceService: BiographyServiceService) { }

  ngOnInit(): void {
    this.getExpDetails();
    this.getCertDetails();
  }

  getExpDetails() {
    this.biographyServiceService.getExperienceDetails().subscribe(
      (data) => {
      this.expDetails = data;
      console.log(this.expDetails);
    });
  }
  
  getCertDetails() {
    this.biographyServiceService.getCertificationList().subscribe(
      (data) => {
      this.certDetails = data;
      console.log(this.certDetails);
    });
  }
}
