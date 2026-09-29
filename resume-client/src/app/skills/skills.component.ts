import { Skill } from './../entity/Skill';
import { Component, OnInit } from '@angular/core';
import { BiographyServiceService } from './../biography-service.service';
import { splitCsv } from './../shared/text-animation';

@Component({
  selector: 'app-skills',
  templateUrl: './skills.component.html',
  styleUrls: ['./skills.component.css']
})
export class SkillsComponent implements OnInit {

  // Matched by substring against the category name (data-driven, not a fixed list), so a new category
  // from the backend just falls back to a generic icon instead of breaking.
  private static readonly ICONS: { [categoryMatch: string]: string } = {
    'language': 'code', 'backend': 'code',
    'cloud': 'cloud', 'container': 'cloud',
    'auth': 'lock', 'identity': 'lock',
    'test': 'bug_report', 'devops': 'bug_report',
    'database': 'storage',
    'frontend': 'web',
    'process': 'groups', 'leadership': 'groups',
    'ai': 'smart_toy'
  };

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

  chipsOf(name: string): string[] {
    return splitCsv(name);
  }

  iconFor(category: string): string {
    const lower = (category || '').toLowerCase();
    const key = Object.keys(SkillsComponent.ICONS).find(k => lower.includes(k));
    return key ? SkillsComponent.ICONS[key] : 'star';
  }

}
