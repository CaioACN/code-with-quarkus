import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AppNotification {
  type: 'resgate_created' | 'data_updated';
  data?: any;
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationSubject = new BehaviorSubject<AppNotification | null>(null);
  
  constructor() { }

  /**
   * Observable para escutar notificações
   */
  getNotifications(): Observable<AppNotification | null> {
    return this.notificationSubject.asObservable();
  }

  /**
   * Notifica que um resgate foi criado
   */
  notifyResgateCreated(resgateData?: any): void {
    this.notificationSubject.next({
      type: 'resgate_created',
      data: resgateData,
      timestamp: new Date()
    });
  }

  /**
   * Notifica que os dados foram atualizados
   */
  notifyDataUpdated(data?: any): void {
    this.notificationSubject.next({
      type: 'data_updated',
      data: data,
      timestamp: new Date()
    });
  }

  /**
   * Limpa a notificação atual
   */
  clearNotification(): void {
    this.notificationSubject.next(null);
  }
}