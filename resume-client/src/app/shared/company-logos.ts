// Front-end only: the backend has no logo field, and these companies are unlikely to change often.
// A company not listed here just shows no logo instead of a broken image. Shared so Home's headline
// and the Work Experience cards show the same logo for the same company.
const LOGOS: { [companyMatch: string]: string } = {
  't-systems': 'assets/logos/t-systems.png',
  'here technologies': 'assets/logos/here-technologies.png',
  'ltimindtree': 'assets/logos/ltimindtree.svg'
};

export function logoForCompany(company: string): string {
  const lower = (company || '').toLowerCase();
  const key = Object.keys(LOGOS).find(k => lower.includes(k));
  return key ? LOGOS[key] : null;
}
