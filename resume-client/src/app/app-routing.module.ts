import { ContactMeComponent } from './contact-me/contact-me.component';
import { PersonalDetailsComponent } from './personal-details/personal-details.component';
import { EducationComponent } from './personal-details/education/education.component';
import { ExperienceComponent } from './experience/experience.component';
import { SkillsComponent } from './skills/skills.component';
import { HomeComponent } from './home/home.component';
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';


const routes: Routes = [
  {path:"",redirectTo:"register",pathMatch:"full"},
   {path:"home",component:HomeComponent},
   {path:"skills",component:SkillsComponent},
   {path:"experience",component:ExperienceComponent},
   {path:"about-me",component:PersonalDetailsComponent},
   {path:"contact-me",component:ContactMeComponent}
];


@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
