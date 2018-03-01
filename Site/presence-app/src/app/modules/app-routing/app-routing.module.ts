import { NgModule } from "@angular/core";
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';
import { PageNotFoundComponent } from "./components/page-not-found/page-not-found.component";

const appRoutes: Routes = [
    { 
        path: '',
        loadChildren: '../Home/home.module#HomeModule'
    },
    { 
        path: 'admin',
        loadChildren: '../Admin/admin.module#AdminModule'
    },
    { path: '**', component: PageNotFoundComponent}
];

@NgModule({
    imports: [
        RouterModule.forRoot(appRoutes,
            {preloadingStrategy: PreloadAllModules})
    ],
    declarations: [],
    exports: [
        RouterModule
    ],
    providers: []
})
export class AppRoutingModule { }