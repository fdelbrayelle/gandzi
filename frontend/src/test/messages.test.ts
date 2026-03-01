import { describe, expect, it } from 'vitest';
import { messages } from '../i18n/messages';

describe('i18n messages', () => {
  it('has english as default locale dictionary', () => {
    expect(messages.en.appTitle).toBeTruthy();
    expect(messages.en.welcome).toBeTruthy();
  });
});
