import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FlagChangeEvent } from './models';

/**
 * Thin wrapper around a single shared STOMP client. The dashboard subscribes
 * per project (see subscribeToProject) and gets pushed FlagChangeEvents the
 * instant the backend broadcasts one — no polling.
 *
 * Connection is lazy: it only opens on the first subscribeToProject call, and
 * is torn down when the last subscriber unsubscribes.
 */
@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private client: Client | null = null;
  private activeSubscriptions = 0;

  subscribeToProject(projectId: string): Observable<FlagChangeEvent> {
    return new Observable<FlagChangeEvent>((subscriber) => {
      this.ensureConnected();

      let stompSub: StompSubscription | null = null;
      const destination = `/topic/projects/${projectId}/flags`;

      const trySubscribe = () => {
        if (!this.client) return;
        stompSub = this.client.subscribe(destination, (message: IMessage) => {
          subscriber.next(JSON.parse(message.body) as FlagChangeEvent);
        });
      };

      if (this.client?.connected) {
        trySubscribe();
      } else if (this.client) {
        this.client.onConnect = () => trySubscribe();
      }

      this.activeSubscriptions++;

      return () => {
        stompSub?.unsubscribe();
        this.activeSubscriptions--;
        if (this.activeSubscriptions <= 0) {
          this.disconnect();
        }
      };
    });
  }

  private ensureConnected(): void {
    if (this.client) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl) as WebSocket,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    });

    this.client.activate();
  }

  private disconnect(): void {
    this.client?.deactivate();
    this.client = null;
  }
}
