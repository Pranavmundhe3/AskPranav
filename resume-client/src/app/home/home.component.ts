import { BiographyServiceService } from './../biography-service.service';
import { Summary } from './../entity/Summary';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  summaryDetails: any;
  summaryInfo: string;

  constructor(private biographyServiceService: BiographyServiceService,
    private router: Router) { }

  ngOnInit(): void {
    this.getSummaryDetails();
  }

  routeToContactMe() {
    this.router.navigateByUrl('/contact-me');
};

  getSummaryDetails() {
    let summary = this.biographyServiceService.getSummaryDetails();
    summary.subscribe((data) => {
      this.summaryDetails = data;
      console.log(this.summaryDetails);
    });
  }

}
