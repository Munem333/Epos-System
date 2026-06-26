const categories = ["All","Beverages","Snacks","Fruits","Electronics","Household"];

const products = [

{id:1,name:"Coca Cola",price:30,category:"Beverages",emoji:"🥤",bg:"linear-gradient(135deg,#3b1219,#1a0a0e)"},

{id:2,name:"Lays Chips",price:40,category:"Snacks",emoji:"🍟",bg:"linear-gradient(135deg,#3b2f0a,#1a1505)"},

{id:3,name:"Banana",price:15,category:"Fruits",emoji:"🍌",bg:"linear-gradient(135deg,#3b3a0a,#1a1905)"},

{id:4,name:"Headphones",price:800,category:"Electronics",emoji:"🎧",bg:"linear-gradient(135deg,#0a2a3b,#051520)"},

{id:5,name:"Coffee",price:60,category:"Beverages",emoji:"☕",bg:"linear-gradient(135deg,#2a1a0a,#150d05)"},

{id:6,name:"Notebook",price:25,category:"Household",emoji:"📓",bg:"linear-gradient(135deg,#1a2a3b,#0d1520)"},

{id:7,name:"Milk",price:55,category:"Beverages",emoji:"🥛",bg:"linear-gradient(135deg,#2a2a3b,#151520)"},

{id:8,name:"Apple",price:20,category:"Fruits",emoji:"🍎",bg:"linear-gradient(135deg,#3b0a0a,#1a0505)"}

];

let cart = [];

let activeCategory = "All";

let currentPage = 1;

const itemsPerPage = 8;

function init(){

renderCategories();

renderProducts();

updateDateTime();

setInterval(updateDateTime,60000);

}

function updateDateTime(){

const now = new Date();

const options = {month:"short",day:"numeric",year:"numeric"};

const date = now.toLocaleDateString("en-US",options);

const time = now.toLocaleTimeString("en-US",{hour:"numeric",minute:"2-digit",hour12:true});

document.getElementById("datetime").textContent = date + " • " + time;

}

function renderCategories(){

const container = document.getElementById("categoryTabs");

container.innerHTML = categories.map(cat =>

`<button class="category-tab ${cat === activeCategory ? "active" : ""}" type="button" onclick="filterCategory('${cat}')">${cat}</button>`

).join("");

}

function filterCategory(category){

activeCategory = category;

currentPage = 1;

renderCategories();

renderProducts();

}

function getFilteredProducts(){

if(activeCategory === "All"){

return products;

}

return products.filter(p => p.category === activeCategory);

}

function renderProducts(){

const filtered = getFilteredProducts();

const totalPages = Math.max(1,Math.ceil(filtered.length / itemsPerPage));

const start = (currentPage - 1) * itemsPerPage;

const pageItems = filtered.slice(start,start + itemsPerPage);

const grid = document.getElementById("productGrid");

if(pageItems.length === 0){

grid.innerHTML = `<p class="cart-empty" style="grid-column:1/-1">No products found in this category.</p>`;

}else{

grid.innerHTML = pageItems.map(product => `

<div class="product-card">

<div class="product-image-wrap" style="background:${product.bg}">

<span class="product-emoji">${product.emoji}</span>

</div>

<h3>${product.name}</h3>

<p class="product-price">৳ ${product.price}</p>

<button class="add-btn" type="button" onclick="addToCart(${product.id})" title="Add ${product.name}">+</button>

</div>

`).join("");

}

renderPagination(totalPages);

}

function renderPagination(totalPages){

const container = document.getElementById("pagination");

let html = `<button class="page-btn" type="button" onclick="changePage(${currentPage - 1})" ${currentPage === 1 ? "disabled" : ""}>&lt;</button>`;

for(let i = 1; i <= totalPages; i++){

html += `<button class="page-btn ${i === currentPage ? "active" : ""}" type="button" onclick="changePage(${i})">${i}</button>`;

}

html += `<button class="page-btn" type="button" onclick="changePage(${currentPage + 1})" ${currentPage === totalPages ? "disabled" : ""}>&gt;</button>`;

container.innerHTML = html;

}

function changePage(page){

const totalPages = Math.max(1,Math.ceil(getFilteredProducts().length / itemsPerPage));

if(page < 1 || page > totalPages){

return;

}

currentPage = page;

renderProducts();

}

function addToCart(productId){

const product = products.find(p => p.id === productId);

if(!product){

return;

}

const existing = cart.find(item => item.id === productId);

if(existing){

existing.qty += 1;

}else{

cart.push({

id:product.id,

name:product.name,

price:product.price,

emoji:product.emoji,

bg:product.bg,

qty:1

});

}

renderCart();

}

function updateQty(productId,delta){

const item = cart.find(i => i.id === productId);

if(!item){

return;

}

item.qty += delta;

if(item.qty <= 0){

removeFromCart(productId);

return;

}

renderCart();

}

function removeFromCart(productId){

cart = cart.filter(item => item.id !== productId);

renderCart();

}

function clearCart(){

if(cart.length === 0){

return;

}

if(confirm("Are you sure you want to clear the cart?")){

cart = [];

renderCart();

}

}

function renderCart(){

const container = document.getElementById("cartItems");

const totalItems = cart.reduce((sum,item) => sum + item.qty,0);

document.getElementById("cartCount").textContent = totalItems;

if(cart.length === 0){

container.innerHTML = `<p class="cart-empty">No items in cart. Add products to get started.</p>`;

}else{

container.innerHTML = cart.map(item => `

<div class="cart-item">

<div class="cart-item-thumb" style="background:${item.bg}">${item.emoji}</div>

<div class="cart-item-info">

<h4>${item.name}</h4>

<div class="qty-controls">

<button class="qty-btn" type="button" onclick="updateQty(${item.id},-1)">−</button>

<span class="qty-value">${item.qty}</span>

<button class="qty-btn" type="button" onclick="updateQty(${item.id},1)">+</button>

</div>

</div>

<span class="cart-item-price">৳ ${item.price * item.qty}</span>

<button class="remove-btn" type="button" onclick="removeFromCart(${item.id})" title="Remove">✕</button>

</div>

`).join("");

}

const subtotal = cart.reduce((sum,item) => sum + item.price * item.qty,0);

const discount = 0;

const total = subtotal - discount;

document.getElementById("subtotal").textContent = subtotal;

document.getElementById("discount").textContent = discount;

document.getElementById("total").textContent = total;

}

function sendData(){

const total = cart.reduce((sum,item) => sum + item.price * item.qty,0);

if(cart.length === 0){

alert("Your cart is empty. Please add at least one product before sending.");

return;

}

const itemList = cart.map(item => item.name + " x" + item.qty + " = ৳" + item.price * item.qty).join("\n");

alert("Order sent to Android POS!\n\n" + itemList + "\n\nTotal = ৳" + total);

}

init();
