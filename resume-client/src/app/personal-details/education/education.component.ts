import { Education } from './../../entity/Education';
import { Component, OnInit } from '@angular/core';
import { BiographyServiceService } from './../../biography-service.service';

@Component({
  selector: 'app-education',
  templateUrl: './education.component.html',
  styleUrls: ['./education.component.css']
})
export class EducationComponent implements OnInit {

  eduDetails: Education;

  constructor(private biographyServiceService: BiographyServiceService) { }

  ngOnInit(): void {
    this.getPersonal();
  }

  getPersonal() {
    this.biographyServiceService.getEducationDetails().subscribe(
      (data) => {
      this.eduDetails = data;
      console.log(this.eduDetails);
    });
  }
}
