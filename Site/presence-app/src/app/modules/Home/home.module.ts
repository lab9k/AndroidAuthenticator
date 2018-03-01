import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpModule } from '@angular/http';
import { RouterModule } from '@angular/router';
import { HomeDataService } from './home.service';
import { CampusListComponent } from './components/campus/campus-list/campus-list.component';
import { CampusComponent } from './components/campus/campus/campus.component';
import { UserListComponent } from './components/user/user-list/user-list.component';
import { UserComponent } from './components/user/user/user.component';

const routes = [
  { path: '', component: CampusListComponent },
];

@NgModule({
  imports: [
    HttpModule,
    CommonModule,
    RouterModule.forChild(routes)
  ],
  declarations: [
    CampusListComponent,
    CampusComponent,
    UserListComponent,
    UserComponent
  ],
  providers: [
    HomeDataService
  ]
})
export class HomeModule { }