import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PTZButtonComponent } from './ptzbutton.component';

describe('PTZButtonComponent', () => {
  let component: PTZButtonComponent;
  let fixture: ComponentFixture<PTZButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PTZButtonComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PTZButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
