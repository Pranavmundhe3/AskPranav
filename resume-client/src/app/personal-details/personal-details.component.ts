import { Personal } from './../entity/Personal';
import { Component, OnInit } from '@angular/core';
import { BiographyServiceService } from './../biography-service.service';

@Component({
  selector: 'app-personal-details',
  templateUrl: './personal-details.component.html',
  styleUrls: ['./personal-details.component.css']
})
export class PersonalDetailsComponent implements OnInit {

  personalDetails: Personal;

  constructor(private biographyServiceService: BiographyServiceService) { }

  ngOnInit(): void {
    this.getPersonal();
  }

  getPersonal() {
    this.biographyServiceService.getPersonalDetails().subscribe(
      (data) => {
      this.personalDetails = data;
      console.log(this.personalDetails);
    });
  }
  
}
