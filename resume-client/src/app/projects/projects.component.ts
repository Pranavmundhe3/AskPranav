import { Component, OnInit } from '@angular/core';
import { BiographyServiceService } from './../biography-service.service';
import { Project } from './../entity/Project';
import { splitCsv } from './../shared/text-animation';

@Component({
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit {

  projects: Project[] = [];
  error = false;

  constructor(private biographyServiceService: BiographyServiceService) { }

  ngOnInit(): void {
    this.biographyServiceService.getProjectDetails().subscribe(
      (data) => this.projects = data,
      () => this.error = true
    );
  }

  /** The backend stores tech stack as one comma-separated string; shown here as separate chips. */
  techsOf(techStack: string): string[] {
    return splitCsv(techStack);
  }
}
