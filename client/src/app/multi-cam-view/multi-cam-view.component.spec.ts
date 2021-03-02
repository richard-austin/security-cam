import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MultiCamViewComponent } from './multi-cam-view.component';

describe('MultiCamViewComponent', () => {
  let component: MultiCamViewComponent;
  let fixture: ComponentFixture<MultiCamViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ MultiCamViewComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MultiCamViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
