import { expect, test } from '@playwright/test'

test('shows the starter page', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Hackathon Starter' })).toBeVisible()
})
