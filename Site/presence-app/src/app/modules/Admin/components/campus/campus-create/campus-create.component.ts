import { Component, OnInit } from '@angular/core';
import { AdminDataService } from '../../../admin.service';

declare var $: any;

@Component({
  selector: 'app-campus-create',
  templateUrl: './campus-create.component.html',
  styleUrls: ['./campus-create.component.css']
})
export class CampusCreateComponent implements OnInit {

  private campusName: String;

  constructor(private adminDataService: AdminDataService) { }

  ngOnInit() {
  }

  createCampus() {
    this.campusName = $("input[name='campus-name']").val();
    if(this.campusName.trim() !== "") {
      this.adminDataService.addCampus(this.campusName).subscribe();
    }

  }
}
