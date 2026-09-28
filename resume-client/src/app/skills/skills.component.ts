import { Skill } from './../entity/Skill';
import { Component, OnInit } from '@angular/core';
import { BiographyServiceService } from './../biography-service.service';

@Component({
  selector: 'app-skills',
  templateUrl: './skills.component.html',
  styleUrls: ['./skills.component.css']
})
export class SkillsComponent implements OnInit {

  // One entry per skill category, as stored in the backend (the categories are data, not hardcoded here).
  skills: Skill[] = [];
  error = false;

  constructor(private biographyServiceService: BiographyServiceService) {
  }

  ngOnInit(): void {
    this.biographyServiceService.getSkills().subscribe(
      (data) => this.skills = data,
      () => this.error = true
    );
  }

}
