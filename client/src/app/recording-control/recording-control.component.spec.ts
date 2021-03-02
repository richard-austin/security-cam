import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecordingControlComponent } from './recording-control.component';

describe('RecordingControlComponent', () => {
  let component: RecordingControlComponent;
  let fixture: ComponentFixture<RecordingControlComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RecordingControlComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RecordingControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
