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
  /** The backend stores hobbies as one comma-separated string; shown here as separate chips. */
  hobbies: string[] = [];
  error = false;

  constructor(private biographyServiceService: BiographyServiceService) { }

  ngOnInit(): void {
    this.getPersonal();
  }

  getPersonal() {
    this.biographyServiceService.getPersonalDetails().subscribe(
      (data) => {
        this.personalDetails = data;
        this.hobbies = ((data && data.hobbies) || '')
          .split(',')
          .map(hobby => hobby.trim())
          .filter(hobby => hobby.length > 0);
      },
      () => this.error = true
    );
  }

}
