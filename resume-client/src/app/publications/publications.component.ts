import { Component, OnInit } from '@angular/core';
import { BiographyServiceService } from './../biography-service.service';
import { Publication } from './../entity/Publication';
import { splitCsv } from './../shared/text-animation';

@Component({
  selector: 'app-publications',
  templateUrl: './publications.component.html',
  styleUrls: ['./publications.component.css']
})
export class PublicationsComponent implements OnInit {

  publications: Publication[] = [];
  error = false;

  constructor(private biographyServiceService: BiographyServiceService) { }

  ngOnInit(): void {
    this.biographyServiceService.getPublicationDetails().subscribe(
      (data) => this.publications = data,
      () => this.error = true
    );
  }

  /** The backend stores technologies as one comma-separated string; shown here as separate chips. */
  techsOf(technologies: string): string[] {
    return splitCsv(technologies);
  }
}
