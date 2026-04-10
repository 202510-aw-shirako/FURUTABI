import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptDir, '..');
const resourcesRoot = path.join(root, 'furutabi', 'src', 'main', 'resources');
const templatesRoot = path.join(resourcesRoot, 'templates');
const staticRoot = path.join(resourcesRoot, 'static');
const docsRoot = path.join(root, 'docs');

const sectionDirs = {
  public: 'public',
  app: 'app',
  auth: 'auth'
};

const routeTargets = new Map([
  ['/about.html', 'public/about.html'],
  ['/bridge.html', 'public/bridge.html'],
  ['/contact-complete.html', 'public/contact-complete.html'],
  ['/contact.html', 'public/contact.html'],
  ['/faq.html', 'public/faq.html'],
  ['/gate-entry.html', 'public/gate-entry.html'],
  ['/gate.html', 'public/gate.html'],
  ['/index.html', 'public/index.html'],
  ['/local.html', 'public/local.html'],
  ['/notice.html', 'public/notice.html'],
  ['/notices.html', 'public/notices.html'],
  ['/okatte-entry.html', 'public/okatte-entry.html'],
  ['/privacy.html', 'public/privacy.html'],
  ['/story.html', 'public/story.html'],
  ['/terms.html', 'public/terms.html'],
  ['/tour-preview.html', 'public/tour-preview.html'],
  ['/tour.html', 'public/tour.html'],
  ['/login', 'auth/login.html'],
  ['/login.html', 'auth/login.html'],
  ['/register', 'auth/register.html'],
  ['/register.html', 'auth/register.html'],
  ['/register-profile', 'auth/register-profile.html'],
  ['/register-profile.html', 'auth/register-profile.html'],
  ['/register-sms', 'auth/register-sms.html'],
  ['/register-sms.html', 'auth/register-sms.html'],
  ['/register-verify', 'auth/register-verify.html'],
  ['/register-verify.html', 'auth/register-verify.html'],
  ['/app/home', 'app/home.html'],
  ['/app/home.html', 'app/home.html'],
  ['/app/local-member-home', 'app/local-member-home.html'],
  ['/app/local-member-home.html', 'app/local-member-home.html'],
  ['/app/map-records', 'app/map-records.html'],
  ['/app/map-records.html', 'app/map-records.html'],
  ['/app/footprints', 'app/footprint-list.html'],
  ['/app/footprints.html', 'app/footprint-list.html'],
  ['/app/gate', 'app/gate-list.html'],
  ['/app/gate.html', 'app/gate-list.html'],
  ['/app/okatte', 'app/okatte-list.html'],
  ['/app/okatte.html', 'app/okatte-list.html'],
  ['/app/chat', 'app/chat-thread-list.html'],
  ['/app/chat.html', 'app/chat-thread-list.html'],
  ['/app/history', 'app/history-list.html'],
  ['/app/history.html', 'app/history-list.html'],
  ['/app/notifications', 'app/notification-list.html'],
  ['/app/notifications.html', 'app/notification-list.html'],
  ['/app/mypage', 'app/mypage.html'],
  ['/app/mypage.html', 'app/mypage.html'],
  ['/app/account', 'app/account.html'],
  ['/app/account.html', 'app/account.html'],
  ['/app/profile', 'app/profile.html'],
  ['/app/profile.html', 'app/profile.html'],
  ['/app/privacy-settings', 'app/privacy-settings.html'],
  ['/app/privacy-settings.html', 'app/privacy-settings.html'],
  ['/app/support', 'app/support-list.html'],
  ['/app/support.html', 'app/support-list.html'],
  ['/app/support/new', 'app/support-form.html'],
  ['/app/admin', 'app/admin-home.html'],
  ['/app/admin/users', 'app/admin-user-list.html']
]);

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function copyDir(sourceDir, targetDir) {
  ensureDir(targetDir);
  for (const entry of fs.readdirSync(sourceDir, { withFileTypes: true })) {
    const sourcePath = path.join(sourceDir, entry.name);
    const targetPath = path.join(targetDir, entry.name);
    if (entry.isDirectory()) {
      copyDir(sourcePath, targetPath);
    } else {
      fs.copyFileSync(sourcePath, targetPath);
    }
  }
}

function splitUrl(value) {
  const hashIndex = value.indexOf('#');
  const queryIndex = value.indexOf('?');
  const cutIndexes = [hashIndex, queryIndex].filter(index => index >= 0);
  const cut = cutIndexes.length ? Math.min(...cutIndexes) : value.length;
  return {
    pathname: value.slice(0, cut),
    suffix: value.slice(cut)
  };
}

function relativeFromSection(section, targetPath) {
  return path.posix.relative(sectionDirs[section], targetPath) || '.';
}

function convertAbsoluteUrl(rawValue, section) {
  if (!rawValue.startsWith('/')) {
    return rawValue;
  }

  if (rawValue.startsWith('/css/')) {
    return relativeFromSection(section, rawValue.slice(1));
  }

  if (rawValue.startsWith('/assets/')) {
    return relativeFromSection(section, path.posix.join('public', rawValue.slice(1)));
  }

  const { pathname, suffix } = splitUrl(rawValue);
  const routeTarget = routeTargets.get(pathname);

  if (!routeTarget) {
    return rawValue.slice(1);
  }

  return relativeFromSection(section, routeTarget) + suffix;
}

function rewriteHtml(content, section) {
  let rewritten = content;

  rewritten = rewritten.replace(
    /<head([^>]*)>/i,
    '<head$1>\n  <meta name="furutabi-export-target" content="docs" />'
  );

  rewritten = rewritten.replace(/(href|src|action)="(\/[^"]*)"/g, (_, attr, value) => {
    return `${attr}="${convertAbsoluteUrl(value, section)}"`;
  });

  rewritten = rewritten.replace(/th:(href|src|action)="@\{([^}]+)\}"/g, (_, attr, value) => {
    const sanitized = value.replace(/\(.*\)$/, '');
    return `${attr}="${convertAbsoluteUrl(sanitized, section)}"`;
  });

  return rewritten;
}

function exportTemplateSection(section) {
  const sourceDir = path.join(templatesRoot, section);
  const targetDir = path.join(docsRoot, section);

  ensureDir(targetDir);

  for (const fileName of fs.readdirSync(sourceDir)) {
    if (!fileName.endsWith('.html')) {
      continue;
    }
    const sourcePath = path.join(sourceDir, fileName);
    const targetPath = path.join(targetDir, fileName);
    const content = fs.readFileSync(sourcePath, 'utf8');
    fs.writeFileSync(targetPath, rewriteHtml(content, section), 'utf8');
  }
}

function main() {
  ensureDir(docsRoot);

  exportTemplateSection('public');
  exportTemplateSection('app');
  exportTemplateSection('auth');

  ensureDir(path.join(docsRoot, 'css'));
  fs.copyFileSync(
    path.join(staticRoot, 'css', 'style.css'),
    path.join(docsRoot, 'css', 'style.css')
  );

  copyDir(
    path.join(staticRoot, 'assets'),
    path.join(docsRoot, 'public', 'assets')
  );

  console.log('Exported docs from src/main/resources to docs/.');
}

main();
