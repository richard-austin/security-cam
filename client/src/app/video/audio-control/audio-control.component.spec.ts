import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AudioControlComponent } from './audio-control.component';

describe('AudioControlComponent', () => {
  let component: AudioControlComponent;
  let fixture: ComponentFixture<AudioControlComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AudioControlComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AudioControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
