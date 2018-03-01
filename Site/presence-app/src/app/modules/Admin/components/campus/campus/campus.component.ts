import { Component, OnInit, Input } from '@angular/core';
import { Campus } from '../../../../../shared/models/campus.model';

@Component({
  selector: 'app-campus',
  templateUrl: './campus.component.html',
  styleUrls: ['./campus.component.css']
})
export class CampusComponent implements OnInit {

  @Input() public campus: Campus;
  
  constructor() { }

  ngOnInit() {
  }

}
