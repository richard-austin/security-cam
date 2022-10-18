import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PTZControlsComponent } from './ptzcontrols.component';

describe('PTZControlsComponent', () => {
  let component: PTZControlsComponent;
  let fixture: ComponentFixture<PTZControlsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PTZControlsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PTZControlsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
