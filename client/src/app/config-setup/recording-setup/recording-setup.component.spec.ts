import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecordingSetupComponent } from './recording-setup.component';

describe('RecordingSetupComponent', () => {
  let component: RecordingSetupComponent;
  let fixture: ComponentFixture<RecordingSetupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecordingSetupComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RecordingSetupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
