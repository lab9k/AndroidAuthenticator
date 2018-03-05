import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppComponent } from './app.component';
import { PageNotFoundComponent } from './modules/app-routing/components/page-not-found/page-not-found.component';
import { AppRoutingModule } from './modules/app-routing/app-routing.module';
import { LoginComponent } from './modules/Login/components/login/login.component';
import { HttpModule } from '@angular/http';
import { AuthenticationService } from './shared/services/authentication.service';
import { AuthGuardService } from './modules/app-routing/auth-guard.service';
import { LocationComponent } from './modules/Checkin/components/location/location.component';

@NgModule({
  declarations: [
    AppComponent,
    PageNotFoundComponent,
  ],
  imports: [
    HttpModule,
    BrowserModule,
    FormsModule,
    AppRoutingModule
  ],
  providers: [
    AuthenticationService,
    AuthGuardService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }