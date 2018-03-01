import { Component, OnInit, Input } from '@angular/core';
import { Location } from '../../../../../shared/models/location.model'

@Component({
  selector: 'app-location',
  templateUrl: './location.component.html',
  styleUrls: ['./location.component.css']
})
export class LocationComponent implements OnInit {

  @Input() public location: Location;
  
  constructor() { }

  ngOnInit() {
  }

}
