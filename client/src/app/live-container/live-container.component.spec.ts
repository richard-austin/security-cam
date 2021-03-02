import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LiveContainerComponent } from './live-container.component';

describe('LiveContainerComponent', () => {
  let component: LiveContainerComponent;
  let fixture: ComponentFixture<LiveContainerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LiveContainerComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LiveContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
