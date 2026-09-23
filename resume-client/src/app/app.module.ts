import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HomeComponent } from './home/home.component';
import { FormsModule } from '@angular/forms';
import { SkillsComponent } from './skills/skills.component';
import { ExperienceComponent } from './experience/experience.component';
import { EducationComponent } from './personal-details/education/education.component';
import { PersonalDetailsComponent } from './personal-details/personal-details.component';
import { ContactMeComponent } from './contact-me/contact-me.component';
import { HttpClientModule } from '@angular/common/http';
import { BiographyServiceService } from './biography-service.service';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatButtonModule } from '@angular/material/button';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    SkillsComponent,
    ExperienceComponent,
    EducationComponent,
    PersonalDetailsComponent,
    ContactMeComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule,
    MatExpansionModule,
    BrowserAnimationsModule,
    MatButtonModule,
    MatDividerModule,
    MatIconModule
  ],
  providers: [BiographyServiceService],
  bootstrap: [AppComponent]
})
export class AppModule { }
