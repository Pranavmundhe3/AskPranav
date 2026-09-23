import { Skill } from './../entity/Skill';
import { Component, OnInit } from '@angular/core';
import { BiographyServiceService } from './../biography-service.service';

@Component({
  selector: 'app-skills',
  templateUrl: './skills.component.html',
  styleUrls: ['./skills.component.css']
})
export class SkillsComponent implements OnInit {

  panelOpenStateTech = false;
  panelOpenStateFrame = false;
  panelOpenStateTool = false;
  panelOpenStateOther = false;
  
  toolSkillDetails: Skill;
  tecSkillDetails: Skill;
  frameSkillDetails: Skill;
  otherSkillDetails: Skill;


  

  constructor(private biographyServiceService: BiographyServiceService) { 
  }

  ngOnInit(): void {
  }

  getSkillsByTech() {
    this.panelOpenStateTech = true;
    this.getSkillsByTec();
  }

  
  getSkillsByTec() {
    this.biographyServiceService.getSkillByType("Technologies").subscribe((data) => {
      this.tecSkillDetails = data;
      console.log(this.tecSkillDetails);
    });
  }

  getSkillsByFramework() {
    this.panelOpenStateFrame = true;
    this.getSkillsByFrame();
  }

  
  getSkillsByFrame() {
    this.biographyServiceService.getSkillByType("Frameworks").subscribe((data) => {
      this.frameSkillDetails = data;
      console.log(this.frameSkillDetails);
    });
  }
  

  getSkillsByTools() {
    this.panelOpenStateTool = true;
    this.getSkillsBytools();
  }
    
  getSkillsBytools() {
    this.biographyServiceService.getSkillByType("Tools").subscribe((data) => {
      this.toolSkillDetails = data;
      console.log(this.toolSkillDetails);
    });
  }


  getSkillsByOthers() {
    this.panelOpenStateOther = true;
    this.getSkillsByOther();
  }

  getSkillsByOther() {
    this.biographyServiceService.getSkillByType("Others").subscribe((data) => {
      this.otherSkillDetails = data;
      console.log(this.otherSkillDetails);
    });
  }

}
